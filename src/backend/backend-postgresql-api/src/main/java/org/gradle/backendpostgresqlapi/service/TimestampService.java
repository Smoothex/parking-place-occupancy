package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.dto.TimestampDto;
import org.gradle.backendpostgresqlapi.entity.ParkingPoint;
import org.gradle.backendpostgresqlapi.entity.Timestamp;
import org.gradle.backendpostgresqlapi.repository.TimestampRepo;
import org.gradle.backendpostgresqlapi.util.DtoConverterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.gradle.backendpostgresqlapi.util.TableNameUtil.TIMESTAMPS;

@Slf4j
@Service
public class TimestampService {

    private final TimestampRepo timestampRepo;

    @Autowired
    public TimestampService(TimestampRepo timestampRepo) {
        this.timestampRepo = timestampRepo;
    }

    /**
     * Creates a timestamp index if it does not already exist.
     */
    public void initializeDbIndex() {
        log.debug("Initializing index for table '{}' ...", TIMESTAMPS);
        timestampRepo.createDbIndex();
        log.info("Index for table '{}' created.", TIMESTAMPS);
    }

    public void saveTimestamp(Timestamp timestamp) {
        ParkingPoint parkingPoint = timestamp.getParkingPoint();
        if (parkingPoint != null && timestampRepo.getMaxOneDuplicate(parkingPoint.getId(), timestamp.getTimestamp()) == 0) {
            timestampRepo.save(timestamp);
        }
    }

    public List<TimestampDto> getAllTimestampsByParkingPointIdAsDto(long parkingPointId) {
        return timestampRepo.getAllByParkingPointId(parkingPointId)
            .stream().map(DtoConverterUtil::convertToDto).toList();
    }
}
