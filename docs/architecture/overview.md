graph TB
    subgraph UI Layer
        MainActivity[MainActivity]
        Screens[Compose Screens]
        Navigation[Navigation Controller]
    end
    
    subgraph ViewModel Layer
        ExerciseVM[ExerciseViewModel]
        WorkoutVM[WorkoutTemplateViewModel]
        LoggerVM[WorkoutLoggerViewModel]
        HistoryVM[HistoryViewModel]
        ProgramVM[ProgramViewModel]
        ActiveCycleVM[ActiveCycleViewModel]
        PRVM[PrViewModel]
        SettingsVM[SettingsViewModel]
        VolumeVM[VolumeViewModel]
    end
    
    subgraph Data Layer
        Database[(Room Database)]
        DataStore[DataStore Preferences]
        
        subgraph DAOs
            ExerciseDao
            TemplateDao
            LoggedWorkoutDao
            ProgramDao
            ActiveCycleDao
            PRDao
        end
        
        subgraph Services
            PrService[PR Detection Service]
            UnitConverter[Unit Converter]
            StrengthAnalytics[Strength Analytics]
        end
    end
    
    UI Layer --> ViewModel Layer
    ViewModel Layer --> DAOs
    ViewModel Layer --> Services
    DAOs --> Database
    SettingsVM --> DataStore
    
    style Database fill:#f9f,stroke:#333,stroke-width:2px
    style DataStore fill:#9ff,stroke:#333,stroke-width:2px