package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.entity.Timestamp;
import org.gradle.backendpostgresqlapi.repository.TimestampRepo;
import org.gradle.backendpostgresqlapi.util.JsonHandler;
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
        if (timestampRepo.getMaxOneDuplicate(timestamp.getParkingPointId(), timestamp.getTimestamp()) == 0) {
            timestampRepo.save(timestamp);
        }
    }

    public List<String> getAllTimestampsByParkingPointId(long parkingPointId) {
        return timestampRepo.getAllByParkingPointId(parkingPointId)
            .stream().map(JsonHandler::convertTimestampToJson).toList();
    }
}
