package com.carrental.system.rentalorder.mail;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.rentalorder.repository.RentalOrderRepository;

@Component
public class RentalOrderMailEventListener {

    private final RentalOrderRepository rentalOrderRepository;
    private final RentalOrderMailService rentalOrderMailService;

    public RentalOrderMailEventListener(
            RentalOrderRepository rentalOrderRepository,
            RentalOrderMailService rentalOrderMailService
    ) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.rentalOrderMailService = rentalOrderMailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void sendOrderSuccessMailAfterCommit(RentalOrderSuccessMailEvent event) {
        try {
            RentalOrderBean order = rentalOrderRepository.findById(event.orderId()).orElse(null);
            rentalOrderMailService.sendOrderSuccessMail(order);
        } catch (Exception e) {
            // 寄信失敗不應該影響訂單本身，先記錄錯誤即可。
            System.err.println("租車訂單成功信寄送失敗，orderId=" + event.orderId() + ", error=" + e.getMessage());
        }
    }
}
