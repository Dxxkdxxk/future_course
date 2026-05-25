package com.lzlz.springboot.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResponse<T> {
    private Long total;
    private List<T> list;
}
