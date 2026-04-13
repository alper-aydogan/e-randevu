package com.erandevu.repository.custom;

import com.erandevu.entity.Appointment;
import com.erandevu.enums.AppointmentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of custom appointment repository.
 * Uses Criteria API for flexible querying and pessimistic locking where necessary.
 */
public class AppointmentRepositoryImpl implements AppointmentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * {@inheritDoc}
     *
     * Finds overlapping appointments using Criteria API for flexibility.
     * Two time ranges overlap when:
     * (start1 < end2) AND (end1 > start2)
     */
    @Override
    public List<Appointment> findOverlappingAppointments(Long doctorId, LocalDateTime startTime, LocalDateTime endTime) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Appointment> cq = cb.createQuery(Appointment.class);
        Root<Appointment> appointment = cq.from(Appointment.class);

        List<Predicate> predicates = new ArrayList<>();

        // Same doctor
        predicates.add(cb.equal(appointment.get("doctor").get("id"), doctorId));

        // Overlapping time range: (appointment.start < endTime) AND (appointment.end > startTime)
        predicates.add(cb.lessThan(appointment.get("appointmentDateTime"), endTime));
        predicates.add(cb.greaterThan(appointment.get("endDateTime"), startTime));

        // Exclude cancelled and no-show appointments
        predicates.add(appointment.get("status").in(AppointmentStatus.SCHEDULED, AppointmentStatus.COMPLETED));

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(appointment.get("appointmentDateTime")));

        return entityManager.createQuery(cq)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    /**
     * {@inheritDoc}
     *
     * Optimized count query for conflict detection without loading entities.
     */
    @Override
    public boolean hasOverlappingAppointments(Long doctorId, LocalDateTime startTime, LocalDateTime endTime) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Appointment> appointment = cq.from(Appointment.class);

        cq.select(cb.count(appointment));

        List<Predicate> predicates = new ArrayList<>();

        // Same doctor
        predicates.add(cb.equal(appointment.get("doctor").get("id"), doctorId));

        // Overlapping time range
        predicates.add(cb.lessThan(appointment.get("appointmentDateTime"), endTime));
        predicates.add(cb.greaterThan(appointment.get("endDateTime"), startTime));

        // Exclude cancelled and no-show
        predicates.add(appointment.get("status").in(AppointmentStatus.SCHEDULED, AppointmentStatus.COMPLETED));

        cq.where(predicates.toArray(new Predicate[0]));

        Long count = entityManager.createQuery(cq)
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .getSingleResult();

        return count > 0;
    }

    /**
     * {@inheritDoc}
     *
     * Uses PESSIMISTIC_WRITE lock for critical operations.
     * Ensures exclusive access during appointment cancellation/modification.
     */
    @Override
    public Optional<Appointment> findByIdWithLock(Long id) {
        String jpql = "SELECT a FROM Appointment a WHERE a.id = :id";

        TypedQuery<Appointment> query = entityManager.createQuery(jpql, Appointment.class);
        query.setParameter("id", id);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        try {
            return Optional.of(query.getSingleResult());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Uses Criteria API for flexible date filtering.
     * Note: @Where annotation handles is_deleted = false automatically.
     */
    @Override
    public long countDailyAppointments(Long doctorId, LocalDateTime date) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Appointment> appointment = cq.from(Appointment.class);

        cq.select(cb.count(appointment));

        List<Predicate> predicates = new ArrayList<>();

        // Same doctor
        predicates.add(cb.equal(appointment.get("doctor").get("id"), doctorId));

        // Same date (from start of day to end of day)
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        predicates.add(cb.greaterThanOrEqualTo(appointment.get("appointmentDateTime"), startOfDay));
        predicates.add(cb.lessThan(appointment.get("appointmentDateTime"), endOfDay));

        // Exclude cancelled and no-show
        predicates.add(appointment.get("status").in(AppointmentStatus.SCHEDULED, AppointmentStatus.COMPLETED));

        cq.where(predicates.toArray(new Predicate[0]));

        Long count = entityManager.createQuery(cq).getSingleResult();
        return count != null ? count : 0L;
    }

    /**
     * {@inheritDoc}
     *
     * Uses Criteria API with pagination support.
     * Note: @Where annotation handles is_deleted = false automatically.
     */
    @Override
    public Page<Appointment> findUpcomingByDoctor(Long doctorId, LocalDateTime fromDate,
                                                   List<AppointmentStatus> statuses, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Main query
        CriteriaQuery<Appointment> cq = cb.createQuery(Appointment.class);
        Root<Appointment> appointment = cq.from(Appointment.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(appointment.get("doctor").get("id"), doctorId));
        predicates.add(cb.greaterThanOrEqualTo(appointment.get("appointmentDateTime"), fromDate));
        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(appointment.get("status").in(statuses));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(appointment.get("appointmentDateTime")));

        TypedQuery<Appointment> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Appointment> content = query.getResultList();

        // Count query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Appointment> countRoot = countCq.from(Appointment.class);
        countCq.select(cb.count(countRoot));
        countCq.where(predicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * {@inheritDoc}
     *
     * Uses Criteria API with pagination support.
     * Note: @Where annotation handles is_deleted = false automatically.
     */
    @Override
    public Page<Appointment> findUpcomingByPatient(Long patientId, LocalDateTime fromDate,
                                                    List<AppointmentStatus> statuses, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Main query
        CriteriaQuery<Appointment> cq = cb.createQuery(Appointment.class);
        Root<Appointment> appointment = cq.from(Appointment.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(appointment.get("patient").get("id"), patientId));
        predicates.add(cb.greaterThanOrEqualTo(appointment.get("appointmentDateTime"), fromDate));
        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(appointment.get("status").in(statuses));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(appointment.get("appointmentDateTime")));

        TypedQuery<Appointment> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Appointment> content = query.getResultList();

        // Count query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Appointment> countRoot = countCq.from(Appointment.class);
        countCq.select(cb.count(countRoot));
        countCq.where(predicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
