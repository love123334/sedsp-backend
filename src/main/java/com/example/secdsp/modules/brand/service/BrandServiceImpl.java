package com.example.secdsp.modules.brand.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.modules.brand.dto.request.CreateBrandRequest;
import com.example.secdsp.modules.brand.dto.request.UpdateBrandRequest;
import com.example.secdsp.modules.brand.dto.response.BrandResponse;
import com.example.secdsp.modules.brand.entity.Brand;
import com.example.secdsp.modules.brand.mapper.BrandMapper;
import com.example.secdsp.modules.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        log.info("Attempting to create brand with name: {}", request.getName());

        validateBrandUniqueness(request.getName(), request.getSlug(), null);

        Brand brand = brandMapper.toEntity(request);
        Brand savedBrand = brandRepository.save(brand);

        log.info("Brand created successfully with ID: {}", savedBrand.getId());
        return brandMapper.toResponse(savedBrand);
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, UpdateBrandRequest request) {
        log.info("Attempting to update brand with ID: {}", id);
        Brand existingBrand = brandRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand", id));

        validateBrandUniqueness(request.getName(), request.getSlug(), id);

        brandMapper.updateEntityFromDto(request, existingBrand);
        Brand updatedBrand = brandRepository.save(existingBrand);

        log.info("Brand updated successfully with ID: {}", updatedBrand.getId());
        return brandMapper.toResponse(updatedBrand);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        log.info("Attempting to soft delete brand with ID: {}", id);

        Brand brand = brandRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand", id));

        brand.setDeletedAt(LocalDateTime.now());
        brandRepository.save(brand);

        log.info("Brand with ID {} soft deleted successfully.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        log.debug("Fetching brand by ID: {}", id);
        Brand brand = brandRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
        return brandMapper.toResponse(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrandResponse> getBrands(String keyword, Pageable pageable) {
        log.debug("Fetching brands with keyword: {} and pageable: {}", keyword, pageable);
        return brandRepository.searchBrands(keyword, pageable)
            .map(brandMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Brand> findEntityById(Long id) {
        return brandRepository.findByIdAndDeletedAtIsNull(id);
    }

    private void validateBrandUniqueness(String name, String slug, Long id) {

        if (id == null) {
            // Create
            if (brandRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
                throw new BusinessException(
                    "Brand with name '" + name + "' already exists."
                );
            }

            if (brandRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(slug)) {
                throw new BusinessException(
                    "Brand with slug '" + slug + "' already exists."
                );
            }

            return;
        }

        if (brandRepository.existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(name, id)) {
            throw new BusinessException(
                "Brand with name '" + name + "' already exists."
            );
        }

        if (brandRepository.existsBySlugIgnoreCaseAndIdNotAndDeletedAtIsNull(slug, id)) {
            throw new BusinessException(
                "Brand with slug '" + slug + "' already exists."
            );
        }
    }
}
