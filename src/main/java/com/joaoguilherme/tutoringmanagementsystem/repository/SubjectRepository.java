package com.joaoguilherme.tutoringmanagementsystem.repository;

import com.joaoguilherme.tutoringmanagementsystem.model.Subject;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    boolean existsByName(String name);

    List<Subject> findAllByOrderByNameAsc();

    List<Subject> findByStatusOrderByNameAsc(SubjectStatus status);
}
