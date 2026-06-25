package com.carrental.system.point.dto;

import java.time.LocalDateTime;

import com.carrental.system.point.enums.ChangeType;
import com.carrental.system.point.enums.VoucherStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * PointsHistory 的本質是一個稽核紀錄（Audit Log），稽核紀錄的設計原則是記錄當下的事實，不依賴其他表的當下狀態。
 * 舉一個實際的問題：如果用 @ManyToOne 關聯 CustomerBean，客戶改了名字之後，歷史紀錄顯示的就是新名字，不是當初那筆紀錄發生時的名字。
 * 業界的稽核紀錄通常會直接把需要的資訊**快照（snapshot）**存進去，例如直接存 cust_name 字串，而不是存外鍵。
 * 若設計時已知此需求-->理想的設計是：在 points_history 表直接加一個 cust_name 欄位，存入當下的客戶姓名
 */


//協助呈現該筆點數異動紀錄對應的客戶姓名
//PointsHistory 是 log 性質的表，設計上本來就不應該跟 CustomerBean 做 JPA 關聯，所以沒有設定@ManyToOne
@Data
@NoArgsConstructor
@AllArgsConstructor	
public class PointsHistoryResponseDTO {
	  private Integer recordId;
	    private Integer custId;
	    private String custName;        // 額外帶入
	    private Integer remainPoints;
	    private ChangeType changeType;
	    private Integer pointsChange;
	    private String referenceId;
	    private String notes;
	    private LocalDateTime createTime;
	    private LocalDateTime expireTime;
	    private Integer availablePoints;//[補充新增]
}
