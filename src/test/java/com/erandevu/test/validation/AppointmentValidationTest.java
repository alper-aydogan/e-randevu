import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;

public class AppointmentValidationTest {

    @Test
    public void testPastDateValidation() {
        LocalDateTime pastAppointmentDate = LocalDateTime.now().minusDays(1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            validateAppointmentDate(pastAppointmentDate);
        });
    }

    @Test
    public void testBusinessHoursValidation() {
        LocalDateTime outsideBusinessHours = LocalDateTime.now().withHour(20).withMinute(0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            validateAppointmentDate(outsideBusinessHours);
        });
    }

    @Test
    public void testWeekendValidation() {
        LocalDateTime weekendDate = LocalDateTime.now().with(DayOfWeek.SATURDAY);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            validateAppointmentDate(weekendDate);
        });
    }

    @Test
    public void testAdvanceBookingLimits() {
        LocalDateTime tooSoonDate = LocalDateTime.now().plusHours(1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            validateAppointmentDate(tooSoonDate);
        });
    }

    private void validateAppointmentDate(LocalDateTime date) {
        LocalDateTime now = LocalDateTime.now();
        if (date.isBefore(now)) {
            throw new IllegalArgumentException("Appointment date cannot be in the past.");
        }
        if (date.getHour() < 9 || date.getHour() > 17) {
            throw new IllegalArgumentException("Appointment must be within business hours (9 AM - 5 PM).");
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Appointments cannot be scheduled on weekends.");
        }
        if (ChronoUnit.HOURS.between(now, date) < 2) {
            throw new IllegalArgumentException("Appointments must be scheduled at least 2 hours in advance.");
        }
    }
}