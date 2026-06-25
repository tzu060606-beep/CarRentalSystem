package com.carrental.system.point.controller;

import com.carrental.system.point.dto.ApiResponse;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.point.entity.Product;
import com.carrental.system.point.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/products")
public class ProductController {

	private final ProductService productService;

	// 新增商品
	@PostMapping
	// 前端把整個 form 物件傳給後端，後端的 Controller 用 @RequestBody Product product 接收，直接存進資料庫。
	public ResponseEntity<ApiResponse> insertProduct(@Valid @RequestBody Product product) {
		Product insertProduct = productService.insertProduct(product);
		return ResponseEntity.ok(new ApiResponse(true, "新增成功", insertProduct));
	}

	// 修改商品
	@PutMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> updateProduct(@PathVariable Integer id, @RequestBody Product product) {
		Product updateProduct = productService.updateProduct(id, product);
		return ResponseEntity.ok(new ApiResponse(true,  "修改Id為" + id + "的商品", updateProduct));
	}

	// 刪除商品
	@DeleteMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Integer id) {
		productService.deleteProductById(id);
		return ResponseEntity.ok(new ApiResponse(true,  "刪除Id為" + id + "的商品", null));
	}

	// 查詢單筆商品
	// http://localhost:8081/api/products/1
	@GetMapping(path = "/{id}")
	public ResponseEntity<ApiResponse> findProduct(@PathVariable Integer id) {
		Product targetProduct = productService.findById(id);
		return ResponseEntity.ok(new ApiResponse(true, "查詢成功", targetProduct));
	}

	// 查詢全部商品
	// http://localhost:8081/api/products
	@GetMapping
	public ResponseEntity<ApiResponse> findAll() {
		List<Product> allProducts = productService.findAllProducts();
		return ResponseEntity.ok(new ApiResponse(true,  "查詢成功", allProducts));
	}

	// 查詢商品關鍵字
	// http://localhost:8081/api/products/search?keyword=輪胎
	@GetMapping(path = "/search")
	public ResponseEntity<ApiResponse> findProductByKeyword(@RequestParam String keyword) {
		List<Product> allProducts = productService.findByKeyword(keyword);
		return ResponseEntity.ok(new ApiResponse(true,  "查詢成功", allProducts));
	}

	// 篩選商品狀態、類別、排序所需點數
	@GetMapping(path = "/filter")
	public ResponseEntity<ApiResponse> findProductByFilters(
			// 三個參數都是選填
			@RequestParam(required = false) String status, 
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String sort) {
		List<Product> filteresProducts = productService.findByFilters(status, category, sort);
		return ResponseEntity.ok(new ApiResponse(true,  "查詢成功", filteresProducts));
	}

	// 【合併為上方方法】
//	// 查詢商品狀態(包含商品上架狀態+庫存，共三個狀態active、outofstock、inactive)
//	// http://localhost:8081/api/products/status?status=active
//	// 前端回傳狀態字串，呼叫findByStatus方法抓出對應的商品資料
//	@GetMapping(path = "/status")
//	public List<Product> findProductByStatus(@RequestParam String status) {
//		List<Product> allProducts = productService.findByStatus(status);
//		return allProducts;
//	}
//
//	// 查詢商品類別
//	// http://localhost:8081/api/products/category?category=汽車配件
//	@GetMapping(path = "/category")
//	public List<Product> findProductByCategory(@RequestParam String category) {
//		List<Product> allProducts = productService.findByCategory(category);
//		return allProducts;
//	}

}
