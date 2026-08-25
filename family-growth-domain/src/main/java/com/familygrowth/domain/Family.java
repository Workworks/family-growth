package com.familygrowth.domain;
import java.time.Instant; import java.util.Objects; import java.util.UUID;
public record Family(UUID id, String name, Instant createdAt) {
  public Family { Objects.requireNonNull(id); Objects.requireNonNull(createdAt); name=requireText(name,"name"); }
  public static Family create(String name, Instant now){ return new Family(UUID.randomUUID(),name,now); }
  static String requireText(String value,String field){ if(value==null||value.isBlank()) throw new IllegalArgumentException(field+" is required"); return value.trim(); }
}
