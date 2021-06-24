package com.boardgamefiesta.domain.featuretoggle;

import com.boardgamefiesta.domain.Repository;

import java.util.Optional;

public interface FeatureToggles extends Repository {

    default FeatureToggle get(FeatureToggle.Id id) {
        return findById(id).orElseThrow(FeatureToggle.NotEnabledException::new);
    }

    Optional<FeatureToggle> findById(FeatureToggle.Id id);

}
