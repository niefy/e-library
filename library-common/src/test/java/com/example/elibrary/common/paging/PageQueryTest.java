package com.example.elibrary.common.paging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void enforcesPageSizeLimit() {
        assertThat(new PageQuery(0, PageQuery.MAX_PAGE_SIZE).size())
                .isEqualTo(PageQuery.MAX_PAGE_SIZE);
        assertThatThrownBy(() -> new PageQuery(0, PageQuery.MAX_PAGE_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageQuery(-1, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computesTotalPages() {
        PageResult<String> result = new PageResult<>(java.util.List.of("a", "b"), 1, 2, 5);
        assertThat(result.totalPages()).isEqualTo(3);
    }
}
