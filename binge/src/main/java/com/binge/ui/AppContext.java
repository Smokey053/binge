package com.binge.ui;

import com.binge.audio.SoundFx;
import com.binge.data.UserLibrary;
import com.binge.model.Catalog;
import javafx.application.HostServices;

/** Shared services handed to every view and component (a simple form of dependency injection). */
public record AppContext(
        Catalog catalog,
        UserLibrary library,
        SoundFx sound,
        Navigator navigator,
        HostServices hostServices) {
}
