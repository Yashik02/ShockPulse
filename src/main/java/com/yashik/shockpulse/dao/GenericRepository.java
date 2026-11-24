package com.yashik.shockpulse.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GenericRepository<T, ID> {
    private final Map<ID, T> storage = new HashMap<>();

    public void store(ID id, T entity) {
        if (id == null || entity == null) {
            throw new IllegalArgumentException("ID and Entity cannot be null.");
        }
        storage.put(id, entity);
    }

    public Optional<T> get(ID id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<T> getAll() {
        return new ArrayList<>(storage.values());
    }

    public int count() {
        return storage.size();
    }
}