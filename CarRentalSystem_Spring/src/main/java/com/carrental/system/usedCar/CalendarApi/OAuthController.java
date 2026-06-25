package com.carrental.system.usedCar.CalendarApi;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
public class OAuthController {

	@GetMapping("/oauth2/success")
	public void oauthSuccess(HttpServletResponse response) throws IOException {
		response.setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups");
		response.setContentType("text/html;charset=UTF-8");

		response.getWriter().write("""
					        <!DOCTYPE html>
					        <html>
					        <body style="text-align:center; padding-top:50px; font-family:sans-serif;">
					            <h3 id="status">授權成功！</h3>
				<p>系統已完成同步準備。</p>
				<button onclick="window.close();" style="padding: 10px 20px; font-size: 16px; cursor: pointer;">
				    關閉視窗
				</button>

					            <script>
    // 給父視窗一點點反應時間
    window.onload = function() {
        if (window.opener) {
            // 每隔 200ms 發送一次，直到確定被收到（或者限制次數）
            var interval = setInterval(function() {
                window.opener.postMessage("GOOGLE_AUTH_SUCCESS", "http://localhost:5173");
            }, 200);

            // 這裡可以加上監聽，收到父視窗確認後才停止發送
            window.addEventListener("message", function(e) {
                if(e.data === "RECEIVED") clearInterval(interval);
            });
        }
    };
</script>
					        </body>
					        </html>
					    """);
	}
}
