package com.carrental.system.point.entity;

import java.time.LocalDateTime;

import com.carrental.system.point.enums.VoucherStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "voucher")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {

	@Id
	@Column(name = "voucher_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer voucherId;

	// --外鍵關聯 redemption_orders FK_voucher_redemption
	@ManyToOne
	@JoinColumn(name = "redemption_id")
	private RedemptionOrder redemptionOrder;

	// @NonNull（Lombok）Java 層呼叫 setter 時如果傳入 null 會拋出 NullPointerException
	// nullable = false（JPA）資料庫層告訴 JPA 這個欄位在資料庫是 NOT NULL
	// --每張券有獨立序號（voucher_code） UNIQUE 約束
	@NonNull
	@Column(name = "voucher_code", nullable = false, unique = true, updatable = false)
	private String voucherCode;

	// --status：UNUSED / USED / EXPIRED DEFAULT 'UNUSED'
	@NonNull
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private VoucherStatus status;

	// --use_time 允許 NULL（使用前為空） DATETIME2 NULL
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(name = "use_time")
	private LocalDateTime useTime;

	// --expiry_date 必填（兌換時 +1年） NOT NULL
	// 兌換訂單建立時間 +365 天
	@NonNull
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(name = "expiry_date", nullable = false, updatable = false)
	private LocalDateTime expiryDate;
}