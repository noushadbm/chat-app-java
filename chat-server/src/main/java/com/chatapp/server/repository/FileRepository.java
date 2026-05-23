package com.chatapp.server.repository;

import com.chatapp.server.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long> {

    Optional<FileMetadata> findByFileId(String fileId);

    List<FileMetadata> findByTimestampLessThan(Long timestamp);

    void deleteByTimestampLessThan(Long timestamp);
}
