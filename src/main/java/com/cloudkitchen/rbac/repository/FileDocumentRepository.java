package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.FileDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileDocumentRepository extends JpaRepository<FileDocument, Integer> {
    
    List<FileDocument> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    List<FileDocument> findByEntityTypeAndEntityIdAndDocumentType(String entityType, String entityId, String documentType);
    
    @Query("SELECT f FROM FileDocument f WHERE f.entityType = :entityType AND f.entityId = :entityId AND f.uploadedByService = :service")
    List<FileDocument> findByEntityAndService(@Param("entityType") String entityType, 
                                            @Param("entityId") String entityId, 
                                            @Param("service") String service);
}