package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.CropRequest;
import com.shopeasy.auth.dto.CropResponse;
import com.shopeasy.auth.dto.CropStageRequest;
import com.shopeasy.auth.dto.CropStageResponse;
import com.shopeasy.auth.entity.Crop;
import com.shopeasy.auth.entity.CropStage;
import com.shopeasy.auth.entity.Product;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.CropRepository;
import com.shopeasy.auth.repository.CropStageRepository;
import com.shopeasy.auth.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CropService {

    private final CropRepository cropRepository;
    private final CropStageRepository cropStageRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CropResponse> list() {
        return cropRepository.findAll().stream().map(this::toResponseWithStages).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CropResponse getById(Long id) {
        return toResponseWithStages(findById(id));
    }

    @Transactional
    public CropResponse create(CropRequest request) {
        Crop crop = Crop.builder()
                .name(request.getName().trim())
                .cropCategory(request.getCropCategory())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();
        return toResponseWithStages(cropRepository.save(crop));
    }

    @Transactional
    public CropResponse update(Long id, CropRequest request) {
        Crop crop = findById(id);
        crop.setName(request.getName().trim());
        crop.setCropCategory(request.getCropCategory());
        crop.setDescription(request.getDescription());
        crop.setImageUrl(request.getImageUrl());
        return toResponseWithStages(cropRepository.save(crop));
    }

    @Transactional
    public void delete(Long id) {
        Crop crop = findById(id);
        cropStageRepository.deleteByCrop(crop);
        cropRepository.delete(crop);
    }

    @Transactional
    public CropStageResponse addStage(Long cropId, CropStageRequest request) {
        Crop crop = findById(cropId);
        Product recommended = request.getRecommendedProductId() != null
                ? productRepository.findById(request.getRecommendedProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recommended product not found"))
                : null;

        CropStage stage = CropStage.builder()
                .crop(crop)
                .stageName(request.getStageName().trim())
                .stageOrder(request.getStageOrder() != null ? request.getStageOrder() : 0)
                .description(request.getDescription())
                .recommendedProduct(recommended)
                .build();
        return CropStageResponse.from(cropStageRepository.save(stage));
    }

    @Transactional
    public CropStageResponse updateStage(Long cropId, Long stageId, CropStageRequest request) {
        Crop crop = findById(cropId);
        CropStage stage = cropStageRepository.findById(stageId)
                .filter(s -> s.getCrop().getId().equals(crop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Crop stage not found"));

        Product recommended = request.getRecommendedProductId() != null
                ? productRepository.findById(request.getRecommendedProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recommended product not found"))
                : null;

        stage.setStageName(request.getStageName().trim());
        if (request.getStageOrder() != null) stage.setStageOrder(request.getStageOrder());
        stage.setDescription(request.getDescription());
        stage.setRecommendedProduct(recommended);

        return CropStageResponse.from(cropStageRepository.save(stage));
    }

    @Transactional
    public void deleteStage(Long cropId, Long stageId) {
        Crop crop = findById(cropId);
        CropStage stage = cropStageRepository.findById(stageId)
                .filter(s -> s.getCrop().getId().equals(crop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Crop stage not found"));
        cropStageRepository.delete(stage);
    }

    private Crop findById(Long id) {
        return cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + id));
    }

    private CropResponse toResponseWithStages(Crop crop) {
        CropResponse response = CropResponse.from(crop);
        response.setStages(cropStageRepository.findByCropOrderByStageOrderAsc(crop).stream()
                .map(CropStageResponse::from).collect(Collectors.toList()));
        return response;
    }
}
