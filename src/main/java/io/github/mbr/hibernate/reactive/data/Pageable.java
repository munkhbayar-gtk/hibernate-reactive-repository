package io.github.mbr.hibernate.reactive.data;

public interface Pageable {

    /**
     * Returns the page to be returned.
     *
     * @return the page to be returned.
     */
    int getPageNumber();

    /**
     * Returns the number of items to be returned.
     *
     * @return the number of items of that page
     */
    int getPageSize();


    /**
     * Returns the sorting parameters.
     *
     * @return
     */
    Sort getSort();
}
