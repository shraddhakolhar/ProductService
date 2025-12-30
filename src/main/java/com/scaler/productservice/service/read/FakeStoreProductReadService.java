package com.scaler.productservice.service.read;

import com.scaler.productservice.entity.ProductEntity;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(
        name = "product.read.provider",
        havingValue = "fakestore"
)
public class FakeStoreProductReadService implements ProductReadService {

    private final RestTemplate restTemplate;

    @Value("${fakestore.api.url:https://fakestoreapi.com/products}")
    private String fakeStoreUrl;

    public FakeStoreProductReadService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Page<ProductEntity> getAll(Pageable pageable) {
        List<ProductEntity> products = fetchAll();
        return toPage(products, pageable);
    }

    @Override
    public ProductEntity getById(Long id) {
        FakeStoreDto dto =
                restTemplate.getForObject(fakeStoreUrl + "/" + id, FakeStoreDto.class);

        if (dto == null) {
            throw new NoSuchElementException("Product not found");
        }
        return dto.toEntity();
    }

    @Override
    public Page<ProductEntity> search(String keyword, Pageable pageable) {
        List<ProductEntity> filtered =
                fetchAll().stream()
                        .filter(p -> p.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                        .collect(Collectors.toList());

        return toPage(filtered, pageable);
    }

    private List<ProductEntity> fetchAll() {
        FakeStoreDto[] response =
                restTemplate.getForObject(fakeStoreUrl, FakeStoreDto[].class);

        if (response == null) return List.of();

        return Arrays.stream(response)
                .map(FakeStoreDto::toEntity)
                .collect(Collectors.toList());
    }

    private Page<ProductEntity> toPage(List<ProductEntity> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());

        if (start > list.size()) {
            return Page.empty(pageable);
        }
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    /* -------- FakeStore DTO (internal only) -------- */
    @Getter
    private static class FakeStoreDto {
        private Long id;
        private String title;
        private String description;
        private double price;
        private String category;
        private String image;

        ProductEntity toEntity() {
            return ProductEntity.builder()
                    .id(id)
                    .title(title)
                    .description(description)
                    .price(price)
                    .category(category)
                    .imageUrl(image)
                    .build();
        }
    }
}
