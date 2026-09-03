package com.ihya.api.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SunnahRepository extends JpaRepository<Sunnah, UUID> {
}
