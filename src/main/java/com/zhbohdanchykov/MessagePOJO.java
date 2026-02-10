package com.zhbohdanchykov;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;


public class MessagePOJO {

    private String name;
    private String eddr;
    private int count;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created_at;
    
    public MessagePOJO() {}

    public MessagePOJO(String name, String eddr, int count, LocalDateTime created_at) {
        this.name = name;
        this.eddr = eddr;
        this.count = count;
        this.created_at = created_at;
    }

    @NotNull(message = "Name must not be null.")
    @NotBlank(message = "Name must not be blank.")
    @Size(min = 7, message = "Name must be equal to 7 characters or longer.")
    @ContainsLetter(value = 'a')
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull(message = "EDDR must not be null")
    @NotBlank(message = "EDDR must not be blank")
    @CheckEddr
    public String getEddr() {
        return eddr;
    }

    public void setEddr(String eddr) {
        this.eddr = eddr;
    }

    @NotNull(message = "Count must not be null.")
    @Min(value = 10, message = "Count must be greater then 10.")
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @NotNull(message = "CreatedAt must not be null.")
    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    @Override
    public String toString() {
        return "MessagePOJO{" +
                "name='" + name + '\'' +
                ", eddr='" + eddr + '\'' +
                ", count=" + count +
                ", created_at=" + created_at +
                '}';
    }
}
