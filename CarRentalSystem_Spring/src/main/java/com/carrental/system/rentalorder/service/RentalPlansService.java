package com.carrental.system.rentalorder.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.rentalorder.dto.AdminPlanReqDto;
import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.rentalorder.entity.RentalPlansBean;
import com.carrental.system.rentalorder.repository.RentalPlansBeanRepository;


@Service
public class RentalPlansService {
    @Autowired
    private RentalPlansBeanRepository planRepos;

    //--查詢單筆
    @Transactional(readOnly = true)
    public RentalPlansBean query(Integer planId){


        RentalPlansBean plan = planRepos.findById(planId)
            .orElseThrow(()->new RuntimeException("找不到資料") );

            return plan;
      
    }


    //--查詢全部
    public List<RentalPlansBean> queryAll(){
        return planRepos.findAll();
        
    }

    

    // --新增

    public RentalPlansBean insert(AdminPlanReqDto planDto){

        //先進行主要資料驗證
        if (planDto.getPlanName() == null || planDto.getPlanName().trim().isEmpty()) {
        throw new IllegalArgumentException("新增失敗：方案名稱不能為空！");
        }
        if (planDto.getIsLongTerm() == null) {
            throw new IllegalArgumentException("新增失敗：必須明確指定是否為長租方案！");
        }
        if (planDto.getBasePrice() == null || planDto.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("新增失敗：基本價格不能為空，且必須大於零！");
        }
        if (planDto.getActive() == null) {
            throw new IllegalArgumentException("新增失敗：必須指定上架狀態！");
        }

        //做狀態的驗證
        // 1.處理超額費用:短租和長租不同

        if(!planDto.getIsLongTerm()){

            if(planDto.getOvertimeFee()==null||planDto.getOvertimeFee().compareTo(BigDecimal.ZERO)<=0){
                throw new IllegalArgumentException("短租要有超額費用");
            }
        }else{
            planDto.setOvertimeFee(BigDecimal.ZERO);//長租沒有超額費
        }

        //處理里程限制和超額的連動

        if(planDto.getMileageLimit()!=null){
            //如果有，不能為負數，且必須有超額費用
            if(planDto.getMileageLimit()<=0){
                throw new IllegalArgumentException("新增失敗：里程限制必須大於零！(若不限里程請直接留空)");
            }else if(planDto.getExcessMileageFee()==null||planDto.getExcessMileageFee().compareTo(BigDecimal.ZERO)<=0){
                throw new IllegalArgumentException("新增失敗：如果有超額里程，超額費用也請一併填入");
            }
        }else{
            planDto.setMileageLimit(0);
            planDto.setExcessMileageFee(BigDecimal.ZERO);

        }
        //先進行覆蓋，維持dto本身的原狀。
        // 解釋
        // 1.避免我們算出值塞進bean後，dto直接塞進bean會導致原先我們算的值背null蓋過(有可能)
        //2.如果我們算出值直接塞入dto是可行的，但規則上希望dto維持原來傳過來的資料
        //3.如果bean有別人的物件(不是id)，那BeanUtil根本不會蓋到那個欄位。
        // 還是要透過dto找一次，最後再塞入bean，所以統一在後面一起塞入。(雖然有時還是會直接塞dto為了方便)

        RentalPlansBean newPlan = new RentalPlansBean();
        BeanUtils.copyProperties(planDto, newPlan);

        //開始塞入資料

        
        
        // 記得補上一些系統預設值
        newPlan.setDeleted(false); 

        return planRepos.save(newPlan);
 
    }

    

    
    
   // --更新
    @Transactional
    public RentalPlansBean update(Integer planId, AdminPlanReqDto planDto) {
        
        // 1. 先把舊資料從資料庫撈出來 (找不到就報錯)
        RentalPlansBean existingPlan = planRepos.findById(planId)
                .orElseThrow(() -> new RuntimeException("更新失敗：找不到指定的方案 ID"));

       
        // 2. 進行主要資料驗證 (比照新增的嚴格標準)

        if (planDto.getPlanName() == null || planDto.getPlanName().trim().isEmpty()) {
            throw new IllegalArgumentException("更新失敗：方案名稱不能為空！");
        }
        if (planDto.getIsLongTerm() == null) {
            throw new IllegalArgumentException("更新失敗：必須明確指定是否為長租方案！");
        }
        if (planDto.getBasePrice() == null || planDto.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("更新失敗：基本價格不能為空，且必須大於零！");
        }
        if (planDto.getActive() == null) {
            throw new IllegalArgumentException("更新失敗：必須指定上架狀態！");
        }

  
        // 3. 狀態連動驗證與資料淨化

        
        // (A) 處理超額費用:短租和長租不同
        if (!planDto.getIsLongTerm()) {
            if (planDto.getOvertimeFee() == null || planDto.getOvertimeFee().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("更新失敗：短租必須設定超額(逾時)費用！");
            }
        } else {
            planDto.setOvertimeFee(BigDecimal.ZERO); // 長租強制沒有超額費
        }

        // (B) 處理里程限制和超額的連動
        if (planDto.getMileageLimit() != null) {
            // 如果有，不能為負數，且必須有超額費用
            if (planDto.getMileageLimit() <= 0) {
                throw new IllegalArgumentException("更新失敗：里程限制必須大於零！(若不限里程請直接留空)");
            } else if (planDto.getExcessMileageFee() == null || planDto.getExcessMileageFee().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("更新失敗：如果有超額里程，超額費用也請一併填入");
            }
        } else {
            planDto.setMileageLimit(0);
            planDto.setExcessMileageFee(BigDecimal.ZERO);
        }

       
        // 4. 進行覆蓋 (直接把洗乾淨的 DTO 蓋到舊實體上)
        
        // 💡 這裡非常安全：因為 planDto 裡面「沒有」 planId 和 createdAt 這類欄位，
        // 所以 BeanUtils 只會覆蓋名稱相同的欄位。你原本舊實體的 ID、建立時間、是否刪除等狀態，
        // 都會完美保留，不會被 null 洗掉！
        BeanUtils.copyProperties(planDto, existingPlan);

        // 5. 存檔並回傳 (因為有加 @Transactional，JPA 其實會自動更新，但顯式呼叫 save 更易讀)
        return planRepos.save(existingPlan);
    }

    
    // --刪除
    @Transactional
    public void delete(Integer planId) {
        
        // 1. 先確認這筆方案存不存在
        if (!planRepos.existsById(planId)) {
            throw new RuntimeException("刪除失敗：找不到指定的方案 ID");
        }
        
        // 2. 執行刪除

        planRepos.deleteById(planId);
    }


    
}
