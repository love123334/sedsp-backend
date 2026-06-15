package com.example.secdsp.modules.category.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryTreeResponse {
     Long id;
     String name;
     String slug;
     List<CategoryTreeResponse> children;
}
