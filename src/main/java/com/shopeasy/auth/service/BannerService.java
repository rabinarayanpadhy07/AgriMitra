package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.BannerRequest;
import com.shopeasy.auth.dto.BannerResponse;
import com.shopeasy.auth.entity.Banner;
import com.shopeasy.auth.exception.ResourceNotFoundException;
import com.shopeasy.auth.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    @Transactional(readOnly = true)
    public List<BannerResponse> publicActive() {
        LocalDate today = LocalDate.now();
        return bannerRepository
                .findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByDisplayOrderAsc(today, today)
                .stream().map(BannerResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> adminList() {
        return bannerRepository.findByOrderByDisplayOrderAsc().stream()
                .map(BannerResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public BannerResponse create(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle().trim())
                .imageUrl(request.getImageUrl())
                .ctaText(request.getCtaText())
                .ctaLink(request.getCtaLink())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() == null || request.getActive())
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .endDate(request.getEndDate() != null ? request.getEndDate() : LocalDate.now().plusYears(1))
                .build();
        return BannerResponse.from(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse update(Long id, BannerRequest request) {
        Banner banner = findById(id);
        banner.setTitle(request.getTitle().trim());
        banner.setImageUrl(request.getImageUrl());
        banner.setCtaText(request.getCtaText());
        banner.setCtaLink(request.getCtaLink());
        if (request.getDisplayOrder() != null) banner.setDisplayOrder(request.getDisplayOrder());
        if (request.getActive() != null) banner.setActive(request.getActive());
        if (request.getStartDate() != null) banner.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) banner.setEndDate(request.getEndDate());
        return BannerResponse.from(bannerRepository.save(banner));
    }

    @Transactional
    public void delete(Long id) {
        bannerRepository.delete(findById(id));
    }

    @Transactional
    public BannerResponse setActive(Long id, boolean active) {
        Banner banner = findById(id);
        banner.setActive(active);
        return BannerResponse.from(bannerRepository.save(banner));
    }

    private Banner findById(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));
    }
}
