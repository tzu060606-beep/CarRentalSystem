package com.carrental.system.point.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import com.carrental.system.point.entity.Product;
import com.carrental.system.point.exception.ProductNotActiveException;
import com.carrental.system.point.exception.ProductNotFoundException;
import com.carrental.system.point.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

	private final ProductRepository productRepository;

	// 新增單筆商品
	public Product insertProduct(Product product) {
		Product newProduct = productRepository.save(product);
		return newProduct;

	}

	// 修改商品
	public Product updateProduct(Integer id, Product product) {
		Optional<Product> targetProduct = productRepository.findById(id);

		Product p = targetProduct.orElseThrow(() -> new ProductNotFoundException("查無id為" + id + "的商品"));
		p.setProductName(product.getProductName());
		p.setDescription(product.getDescription());
		p.setPointsRequired(product.getPointsRequired());
		p.setStockQuantity(product.getStockQuantity());
		p.setIsActive(product.getIsActive());
		p.setImageUrl(product.getImageUrl());
		p.setCategory(product.getCategory());
		Product updateProduct = productRepository.save(p);
		return updateProduct;

	}

	// 刪除商品
	public void deleteProductById(Integer id) {
		Product product = productRepository.findById(id)
			.orElseThrow(() -> new ProductNotFoundException("查無id為" + id + "的商品"));
		//驗證:上架中的商品不可刪除
	    if (product.getIsActive()) {
	        throw new ProductNotActiveException("上架中的商品不可刪除，\n請於確認下架後再執行刪除");
	    }
		productRepository.deleteById(id);
	}

	// 查詢單筆商品
	public Product findById(Integer id) {
		Optional<Product> targetProduct = productRepository.findById(id);
		// 只回傳optional或null
		return targetProduct.orElseThrow(() -> new ProductNotFoundException("查無id為" + id + "的商品"));
	}

	// 查詢全部商品
	public List<Product> findAllProducts() {
		List<Product> productList = productRepository.findAll();
		return productList;
	}

	// 查詢商品關鍵字
	public List<Product> findByKeyword(String keyword) {
		List<Product> productList = productRepository.findByKeyword(keyword);
		return productList;
	}

	// 篩選商品狀態、類別、排序所需點數
	// 三個參數要獨立判斷，三個都處理好，再一起傳進去
	public List<Product> findByFilters(String status, String category, String sort) {
		// status 為空就傳 "all"
		if (status == null || status.isEmpty()) {
			status = "all";
		}
		// category 為空就傳 null
		if (category == null || category.isEmpty()) {
			category = null;
		}
		// sort 為空就傳 "none"
		// Q: 為什麼sort為空不能也回傳null?
		// A: 以其實 sort 傳 null 也可以，結果是一樣的。但傳 "none" 比傳 null
		// 更清楚，讓讀程式碼的人知道「這是刻意不排序」，不是「忘記傳值」。
		if (sort == null || sort.isEmpty()) {
			sort = "none";
		}
		// 完全沒有任何篩選，要回傳所有嗎?-->其實可以-->findAll()就可以刪掉了
		return productRepository.findByFilters(status, category, sort);
	}

	/// Q: 如果後台最後決定都要採用前端篩選，後端篩選要刪掉嗎?
	/// A: 不管是否放在前端篩選，後端都要提供篩選功能。
	/// A: 任何一個會長期增長的資料集，在提供列表接口的時候就要提供篩選、排序以及分頁參數。
	/// Q: 前台後台的篩選使用前端還是後端的基準
	/// A: 安全性、資料傳輸量、即時性、實作複雜度

//	【所有篩選、排序統一合併到上方寫法】	
//	// 查詢商品狀態(綜合上架狀態與庫存去判斷)
//	// 讓前端回傳"active"、"outofstock"、"inactive"這三個狀態來觸發判斷
//	public List<Product> findByStatus(String status) {
//		
//		//上架中、庫存>0-->active
//		if ("active".equals(status)) {
//			return productRepository.findActiveProducts();
//		//缺貨補貨中、庫存為0-->outofstock
//		} else if ("outofstock".equals(status)) {
//			return productRepository.findOutOfStockProducts();
//		//下架中-->inactive
//		} else if ("inactive".equals(status)) {
//			return productRepository.findByIsActiveFalse();
//		}
//		return productRepository.findAll();
//	}
//
//	// 查詢商品類別
//	public List<Product> findByCategory(String category) {
//		List<Product> productList = productRepository.findByCategory(category);
//		return productList;
//	}

}
