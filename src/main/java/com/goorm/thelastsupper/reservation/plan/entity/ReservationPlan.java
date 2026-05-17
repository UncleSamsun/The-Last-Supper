package com.goorm.thelastsupper.reservation.plan.entity;

import com.goorm.thelastsupper.common.entity.BaseEntity;
import com.goorm.thelastsupper.reservation.slot.entity.ReservationSlot;
import com.goorm.thelastsupper.reservation.slot.entity.SlotStatus;
import com.goorm.thelastsupper.reservation.plan.dto.OpenSlotsCommandRequest;
import com.goorm.thelastsupper.reservation.plan.dto.ReservationPlanResponse;
import com.goorm.thelastsupper.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.time.*;
import java.util.*;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * 예약 계획 엔티티 클래스
 * - 예약 슬롯을 관리하는 계획을 나타냅니다.
 */
@Entity
@Getter
@Table(name = "reservation_plan")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationPlan extends BaseEntity {

    /* -------- 필드 -------- */
    /* ――― Restaurant 연관 ――― */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id",
        foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private Restaurant restaurant;
    private LocalDate planDate;

    @Enumerated(EnumType.STRING)
    private DayOfWeek weekday;

    private LocalTime  openTime;
    private LocalTime  closeTime;
    private LocalTime  breakOpenTime;
    private LocalTime  breakCloseTime;
    private long       turnTimeMinutes;

    @OneToMany(mappedBy = "plan",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY)
    private final List<ReservationSlot> slots = new ArrayList<>();

    /* JPA 보호용 생성자 */
    private ReservationPlan(Restaurant restaurant,
        LocalDate planDate,
        DayOfWeek weekdays,
        LocalTime openTime, LocalTime closeTime,
        LocalTime breakOpenTime, LocalTime breakCloseTime,
        long turnTimeMinutes) {
        this.restaurant       = restaurant;
        this.planDate        = planDate;
        this.weekday          = weekdays;
        this.openTime         = openTime;
        this.closeTime        = closeTime;
        this.breakOpenTime    = breakOpenTime;
        this.breakCloseTime   = breakCloseTime;
        this.turnTimeMinutes  = turnTimeMinutes;
    }

    /* -------- 팩터리 메소드 -------- */
    /**
     * 예약 계획을 생성하는 팩터리 메소드
     */
    public static ReservationPlan of(Restaurant restaurant,
        OpenSlotsCommandRequest c) {
        return new ReservationPlan(
            restaurant,
            c.startDate(),
            c.startDate().getDayOfWeek(),
            c.openTime(),
            c.closeTime(),
            c.breakTimes().isEmpty() ? null : c.breakTimes().get(0).start(),
            c.breakTimes().isEmpty() ? null : c.breakTimes().get(0).end(),
            c.slotDuration()
        );
    }

    /* -------- 슬롯 자동 생성 -------- */
    /**
     * 예약 슬롯을 자동으로 생성하는 메소드
     * - 예약 가능한 시간대에 맞춰 예약 슬롯을 생성하고, 브레이크 타임은 슬롯의 상태를 "블락"으로 변경
     *
     * @param cmd 예약 슬롯 생성 명령 객체
     * @return 예약 슬롯 리스트
     */
    public List<ReservationSlot> createSlots(OpenSlotsCommandRequest cmd) {
        // 1) 건너뛸 날이면 빈 리스트 반환
        boolean skipByWeekday =
            cmd.dayOfWeekBased() && !cmd.repeatDays().contains(weekday);
        boolean skipByException =
            cmd.isExceptionDateEnabled()
                && cmd.exceptionDates() != null
                && cmd.exceptionDates().contains(planDate);

        if (skipByWeekday || skipByException) {
            return List.of();
        }

        // 2) 정상 날: 한 번도 생성된 적 없으면 새로 생성
        if (this.slots.isEmpty()) {
            createSlotsForDate(planDate, cmd.capacity(), this.slots);
        }

        // 3) 읽기 전용 반환
        return Collections.unmodifiableList(this.slots);
    }

    /**
     * 특정 날짜에 대해 예약 슬롯을 생성하는 메소드
     * - 해당 날짜의 시작 시간부터 종료 시간까지 예약 슬롯을 생성하고, 브레이크 타임을 고려하여 상태를 "블락"으로 설정
     *
     * @param date 예약 날짜
     * @param capacity 슬롯의 수용 가능 인원
     * @param slotList 생성된 예약 슬롯을 저장할 리스트
     */
    private List<ReservationSlot> createSlotsForDate(LocalDate date, int capacity, List<ReservationSlot> slotList) {
        LocalDateTime cursor = LocalDateTime.of(date, openTime);  // 예약 시작 시간 설정

        // 종료 시간까지 반복하여 슬롯 생성
        while (!cursor.plusMinutes(turnTimeMinutes).toLocalTime().isAfter(closeTime)) {
            // 다음 슬롯의 시작 시간 계산
            LocalDateTime next = cursor.plusMinutes(turnTimeMinutes);

            // 브레이크 타임에 포함되는지 여부를 판단
            boolean inBreak = isInBreakTime(cursor, next);

            // 브레이크 타임이면 슬롯을 "블락" 상태로 변경
            if (inBreak) {
                slotList.add(ReservationSlot.of(
                    this, date, cursor.toLocalTime(), SlotStatus.BLOCK, capacity));
            } else {
                // 일반 슬롯 생성
                slotList.add(ReservationSlot.of(
                    this, date, cursor.toLocalTime(), SlotStatus.OPEN, capacity));
            }
            cursor = next;  // 다음 슬롯의 시작 시간으로 이동
        }
        return slotList;
    }

    /**
     * 주어진 시간 범위가 브레이크 시간에 포함되는지 확인하는 메소드
     *
     * @param cursor 현재 슬롯 시작 시간
     * @param next   다음 슬롯 시작 시간
     * @return 브레이크 시간에 포함되면 true, 아니면 false
     */
    private boolean isInBreakTime(LocalDateTime cursor, LocalDateTime next) {
        return breakOpenTime != null &&
            !(next.toLocalTime().isBefore(breakOpenTime.plusMinutes(turnTimeMinutes))  // 브레이크 시간 이전
                || cursor.toLocalTime().isAfter(breakCloseTime.minusMinutes(turnTimeMinutes)));  // 브레이크 시간 이후
    }

    /* -------- 예약 계획에 날짜 설정하는 메서드 -------- */
    public void setPlanDates(LocalDate date) {
        this.planDate = date;
        this.weekday = date.getDayOfWeek();
    }

    public ReservationPlanResponse toDTO() {
        return new ReservationPlanResponse(
            this.planDate,
            this.weekday,
            this.openTime,
            this.closeTime,
            this.breakOpenTime,
            this.breakCloseTime,
            this.turnTimeMinutes,
            this.slots.stream()
                .map(ReservationSlot::toDTO)
                .toList()
        );
    }

}
