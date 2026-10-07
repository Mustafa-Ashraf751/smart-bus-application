package com.verysmartbus.seeders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class SeedRunner implements CommandLineRunner {

    private final ReferenceDataSeeder referenceDataSeeder;
    private final DevDataSeeder devDataSeeder;

    @Override
    public void run(String... args) {
        System.out.println("========== SEED RUNNER STARTED ==========");

        log.info("Seeder: running seeders...");

        referenceDataSeeder.seed();
        devDataSeeder.seed();

        log.info("Seeder: done.");

        System.out.println("========== SEED RUNNER FINISHED ==========");
    }
}

