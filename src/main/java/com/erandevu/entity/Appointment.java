package com.erandevu.entity;

import com.erandevu.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "appointment_datetime"}))
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"version"})
@ToString(callSuper = false, exclude = {"version"})
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE appointments SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Appointment extends BaseEntity {

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(name = "appointment_datetime", nullable = false)
    private LocalDateTime appointmentDateTime;

    @Column(name = "end_datetime")
    private LocalDateTime endDateTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @PrePersist
    @PreUpdate
    protected void calculateEndDateTime() {
        if (appointmentDateTime != null && endDateTime == null) {
            endDateTime = appointmentDateTime.plusMinutes(30);
        }
    }

    // ==================== DOMAIN BEHAVIOR METHODS ====================

    /**
     * Cancels the appointment with a reason.
     * Domain rule: Cannot cancel already cancelled or completed appointments.
     *
     * @param reason cancellation reason
     * @throws IllegalStateException if appointment cannot be cancelled
     */
    public void cancel(String reason) {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Appointment is already cancelled");
        }
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed appointment");
        }
        if (this.appointmentDateTime.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalStateException("Cannot cancel appointments less than 2 hours before scheduled time");
        }

        this.status = AppointmentStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    /**
     * Reschedules the appointment to a new date/time.
     * Domain rule: Only scheduled appointments can be rescheduled.
     *
     * @param newDateTime new appointment date/time
     * @throws IllegalStateException if appointment cannot be rescheduled
     */
    public void reschedule(LocalDateTime newDateTime) {
        if (this.status != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled appointments can be rescheduled. Current status: " + this.status
            );
        }
        if (newDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("New appointment time must be in the future");
        }

        this.appointmentDateTime = newDateTime;
        this.endDateTime = null; // Will be recalculated by @PreUpdate
    }

    /**
     * Marks the appointment as completed.
     * Domain rule: Only scheduled appointments can be completed.
     *
     * @throws IllegalStateException if appointment cannot be completed
     */
    public void markCompleted() {
        if (this.status != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled appointments can be completed. Current status: " + this.status
            );
        }
        this.status = AppointmentStatus.COMPLETED;
    }

    /**
     * Marks the appointment as no-show.
     * Domain rule: Only scheduled appointments can be marked as no-show.
     *
     * @throws IllegalStateException if appointment cannot be marked as no-show
     */
    public void markNoShow() {
        if (this.status != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled appointments can be marked as no-show. Current status: " + this.status
            );
        }
        this.status = AppointmentStatus.NO_SHOW;
    }

    /**
     * Checks if this appointment overlaps with another time range.
     *
     * @param otherStart other appointment start time
     * @param otherEnd other appointment end time
     * @return true if appointments overlap
     */
    public boolean overlapsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return this.appointmentDateTime.isBefore(otherEnd) &&
               this.endDateTime.isAfter(otherStart);
    }

    /**
     * Checks if appointment can be modified.
     *
     * @return true if appointment is in SCHEDULED status
     */
    public boolean canBeModified() {
        return this.status == AppointmentStatus.SCHEDULED;
    }

    /**
     * Checks if appointment is active (not cancelled or no-show).
     *
     * @return true if appointment is active
     */
    public boolean isActive() {
        return this.status == AppointmentStatus.SCHEDULED ||
               this.status == AppointmentStatus.COMPLETED;
    }
}
