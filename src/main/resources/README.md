# 📊 ObjectId vs Int64 vs Decimal128 in MongoDB & Java

When converting or compacting identifiers, it’s important to understand how much space each representation consumes and what numeric ranges are available.

---

## 1. ObjectId
- **Definition**: The default MongoDB identifier.
- **Size**: **12 bytes = 96 bits**.
- **Layout**:
    - 4 bytes → Timestamp (seconds since epoch)
    - 5 bytes → Machine + Process identifier
    - 3 bytes → Counter (incrementing)
- **Properties**:
    - Sorts roughly chronologically (because timestamp comes first).
    - Provides uniqueness across machines/processes.
- **Range**:
    - Effectively a 96-bit unsigned integer (≈ up to `7.9e28`).

---

## 2. Int64 (`long`)
- **Definition**: 64-bit signed integer type in Java and BSON.
- **Size**: **8 bytes = 64 bits**.
- **Properties**:
    - In Java: `long` is always 64 bits.
    - In MongoDB: stored as `NumberLong`, exactly 8 bytes.
- **Range**:
    - `−9,223,372,036,854,775,808` to `+9,223,372,036,854,775,807`
    - Approx `±9.22e18`.

---

## 3. Decimal128
- **Definition**: IEEE-754-2008 Decimal floating-point with 128 bits.
- **Size**: **16 bytes = 128 bits**.
- **Properties**:
    - MongoDB stores it in BSON as exactly 16 bytes.
    - Supports up to **34 significant decimal digits**.
    - Perfect for high-precision financial or arbitrary identifiers that exceed `int64`.
- **Range**:
    - Approx `±1.0e6145` (far beyond practical needs).

---

# ObjectId vs Int64 vs Decimal128 — Size & Range

| Representation        | Size (bytes) | Size (bits) | Precision / Digits            | Minimum Value                               | Maximum Value                                               | Notes |
|-----------------------|--------------|-------------|-------------------------------|---------------------------------------------|-------------------------------------------------------------|-------|
| **ObjectId (as bytes)** | 12           | 96          | ~29 decimal digits (2^96)     | 0 (if viewed as unsigned)                   | 2^96 − 1 = 79,228,162,514,264,337,593,543,950,335           | BSON `ObjectId` is not a numeric type, but its 12 bytes can be viewed as a 96-bit unsigned integer or converted to a `BigInteger`. |
| **Int64 / `long`**    | 8            | 64          | ~19 decimal digits            | −9,223,372,036,854,775,808                  | 9,223,372,036,854,775,807                                   | Signed 64-bit integer in Java/BSON (`NumberLong`). |
| **Decimal128**        | 16           | 128         | **34 significant decimal digits** | ~ −9.999… × 10^6144 (finite)                | ~ 9.999… × 10^6144 (finite)                                  | IEEE-754 Decimal128; BSON fixed 16 bytes; ideal for high-precision IDs/values up to 34 digits. |

# ObjectIdConverter

## Overview

`ObjectIdConverter` is a small utility that converts a MongoDB `ObjectId` (12 bytes) into:

- A **BigInteger** (Java) or `*big.Int` (Go) that fully represents the 12-byte ObjectId as a positive integer (96 bits).
- A compact **64-bit integer** derived from the ObjectId (lossy, but timestamp-sortable).
- The **timestamp (seconds)** embedded in the ObjectId.

This makes it easier to work with MongoDB `ObjectId` values in systems that expect numeric-only identifiers.

---

## BSON ObjectId Layout

A MongoDB ObjectId is 12 bytes:

[ 4-byte timestamp ][ 5-byte machine+process ][ 3-byte counter ]

The **64-bit integer** is constructed as:

id64 = (timestampSeconds << 32) | (counter24 << 8) | nodeHash8


Where:

- **timestampSeconds (32 bits)**: first 4 bytes (big-endian)
- **counter24 (24 bits)**: last 3 bytes
- **nodeHash8 (8 bits)**: fold/hash of 5 middle bytes

This makes the ID sortable by time while retaining some uniqueness entropy.

---

## Java API

```java
// Full 96-bit representation (no info loss)
public static BigInteger generateBigInteger(ObjectId objectId);

// Time-sortable 64-bit numeric ID (lossy, may collide)
public static Long generateLong(ObjectId objectId);

ObjectId oid = new ObjectId();

BigInteger big = ObjectIdConverter.generateBigInteger(oid);
long id64 = ObjectIdConverter.generateLong(oid);

System.out.println("ObjectId: " + oid.toHexString());
System.out.println("BigInteger: " + big);
System.out.println("Int64 derived: " + id64);
```

## Go Implementation

```go
package objectidconv

import (
	"errors"
	"math/big"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

// GenerateBigInt converts ObjectID into *big.Int (96-bit, no info loss).
func GenerateBigInt(oid primitive.ObjectID) *big.Int {
	b := new(big.Int)
	b.SetBytes(oid[:])
	return b
}

// GenerateInt64 derives a 64-bit sortable numeric id (lossy).
func GenerateInt64(oid primitive.ObjectID) int64 {
	b := oid

	// 1) Timestamp (first 4 bytes, big-endian)
	tsSec := (uint64(b[0]) << 24) | (uint64(b[1]) << 16) |
		(uint64(b[2]) << 8) | uint64(b[3])

	// 2) Machine+process 5 bytes → fold to 8-bit
	nodeHash := foldTo8Bit(b[4:9])

	// 3) Counter (last 3 bytes)
	counter24 := (uint64(b[9]) << 16) | (uint64(b[10]) << 8) | uint64(b[11])

	// Compose final 64-bit id
	id64 := (tsSec << 32) | (counter24 << 8) | uint64(nodeHash)
	return int64(id64)
}

// ExtractTimestampSeconds recovers the timestamp (seconds) from the 64-bit id.
func ExtractTimestampSeconds(id64 int64) uint32 {
	return uint32(uint64(id64) >> 32)
}

// BigIntToDecimal128 converts big.Int to Decimal128.
func BigIntToDecimal128(x *big.Int) (primitive.Decimal128, error) {
	if x == nil {
		return primitive.Decimal128{}, errors.New("nil *big.Int")
	}
	return primitive.ParseDecimal128(x.String())
}

// ObjectIDToDecimal128 convenience: ObjectID -> Decimal128.
func ObjectIDToDecimal128(oid primitive.ObjectID) (primitive.Decimal128, error) {
	return BigIntToDecimal128(GenerateBigInt(oid))
}

// foldTo8Bit creates a small hash of 5 middle bytes.
func foldTo8Bit(p []byte) byte {
	var h uint16 = 0
	for _, v := range p {
		h = (h*31 + uint16(v)) & 0xFF
	}
	if h == 0 {
		return 1
	}
	return byte(h)
}
```


