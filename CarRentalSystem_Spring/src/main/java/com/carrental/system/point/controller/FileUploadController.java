package com.carrental.system.point.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.carrental.system.point.dto.ApiResponse;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

	// 【A】接收前端傳來的檔案名稱與檔案本體 後端用 MultipartFile 接收
	@PostMapping
	public ResponseEntity<ApiResponse> multipartResolver(@RequestParam("file") MultipartFile mf) throws IOException {

		byte[] bytes = mf.getBytes();
		String contentType = mf.getContentType();

		// 【B】定義一個base64字串
		String base64 = Base64.getEncoder().encodeToString(bytes);

		// 【C】加上圖片格式前綴
		String base64WithPrefix = "data:" + contentType + ";base64," + base64;

		// 【D】把原始圖片名稱，base64前綴字串存入map
		String originalFilename = mf.getOriginalFilename();
		Map<String, String> result = new HashMap<String, String>();
		result.put("base64", base64WithPrefix);
		result.put("fileName", originalFilename);

		// 【E】回傳Map物件給前端
		return ResponseEntity.ok(new ApiResponse(true, "上傳成功", result));

	}

	// 接收 Base64 並存成實體檔案
	@PostMapping("/base64")
	public ResponseEntity<ApiResponse> uploadBase64(@RequestBody Map<String, String> payload) {
		try {
			String base64Image = payload.get("base64");
			if (base64Image == null || base64Image.isEmpty()) {
				return ResponseEntity.badRequest().body(new ApiResponse(false, "沒有提供圖片", null));
			}

			// 解析 Base64，移除前綴 "data:image/png;base64,"
			String[] parts = base64Image.split(",");
			String imageString = parts.length > 1 ? parts[1] : parts[0];

			// 取得副檔名
			String extension = ".jpg";
			if (parts.length > 1) {
				String prefix = parts[0];
				if (prefix.contains("png"))
					extension = ".png";
				else if (prefix.contains("gif"))
					extension = ".gif";
				else if (prefix.contains("webp"))
					extension = ".webp";
			}

			byte[] imageBytes = Base64.getDecoder().decode(imageString);

			// 確保 uploads 目錄存在
			File directory = new File("./uploads/");
			if (!directory.exists()) {
				directory.mkdirs();
			}

			// 生成唯一檔名
			String fileName = UUID.randomUUID().toString() + extension;
			File targetFile = new File(directory, fileName);

			// 寫入檔案
			Files.write(targetFile.toPath(), imageBytes);

			// 準備回傳資料
			Map<String, String> result = new HashMap<>();
			String fileUrl = "http://localhost:8081/uploads/" + fileName;
			result.put("url", fileUrl);
			result.put("fileName", fileName);

			return ResponseEntity.ok(new ApiResponse(true, "檔案上傳成功", result));

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(new ApiResponse(false, "上傳失敗：" + e.getMessage(), null));
		}
	}

}
