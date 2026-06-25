import axios from "axios";

/**
 * 封裝綠界跳轉邏輯
 * @param {string} category - 業務標籤 (例如: 'SALES', 'RENTAL', 'TRANSFER')
 * @param {number} id - 對應資料庫的訂單/紀錄 ID
 * @param {string} customItemName - (選填) 要顯示在綠界頁面的商品名稱
 */
const startPayment = async (category, id, customItemName = "") => {
  try {
    // 1. 呼叫後端 API 取得綠界加密參數
    // 加入 customItemName 傳給後端的 PaymentController
    const response = await axios.post("/api/payment/checkout", {
      category: category,
      id: id,
      customItemName: customItemName, // 傳遞給後端的 RequestBody
    });

    const params = response.data;

    // 2. 動態建立隱藏表單 (ECPay 規範必須用 POST 提交到他們的網址)
    const form = document.createElement("form");
    form.method = "POST";
    form.action = "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5"; // 測試環境網址
    form.target = "_self";

    // 3. 將後端回傳的參數填入 input
    Object.keys(params).forEach((key) => {
      const input = document.createElement("input");
      input.type = "hidden";
      input.name = key;
      input.value = params[key];
      form.appendChild(input);
    });

    // 4. 提交並跳轉
    document.body.appendChild(form);
    form.submit();
    // 提交後移除表單，保持頁面乾淨
    document.body.removeChild(form);
  } catch (error) {
    console.error("金流初始化失敗:", error);
    alert("無法啟動支付流程，請檢查後端連線或參數格式。");
  }
};

export default { startPayment };
