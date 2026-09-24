package com.shopeasy.auth.repository;

import com.shopeasy.auth.entity.Crop;
import com.shopeasy.auth.entity.CropStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropStageRepository extends JpaRepository<CropStage, Long> {
    List<CropStage> findByCropOrderByStageOrderAsc(Crop crop);
    void deleteByCrop(Crop crop);
}
