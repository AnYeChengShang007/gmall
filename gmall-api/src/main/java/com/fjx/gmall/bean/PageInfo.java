package com.fjx.gmall.bean;

import java.io.Serializable;

public class PageInfo implements Serializable {

    private long currentPage;

    private long size;

    private long total;

    private long lastPage;

    private long totalPage;

    private int[] pages;

    private boolean isFirst;

    private  boolean isLast;



    public void setPages() {
        if (totalPage < 5) {
            pages = new int[(int) totalPage];
            for (int i = 0; i < totalPage; i++) {
                pages[i] = i;
            }
        } else {
            pages[2] = (int) currentPage;
            for (int i = (int) (currentPage - 1), n = 2; i >= 0 && n > 0; i--, n--) {
                pages[i] = i;
            }
            for (int i = (int) (currentPage + 1), n = 2; i < totalPage && n > 0; i++, n--) {
                pages[i] = i;
            }
        }


    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public int[] getPages() {
        return pages;
    }

    public long getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(long currentPage) {
        this.currentPage = currentPage;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getLastPage() {
        return lastPage;
    }

    public void setLastPage(long lastPage) {
        this.lastPage = lastPage;
    }

    public long getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(long totalPage) {
        this.totalPage = totalPage;
    }
}
