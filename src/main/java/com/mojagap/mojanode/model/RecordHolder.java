package com.mojagap.mojanode.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecordHolder<T> {
    private Integer totalRecords;
    private List<T> records;
}
