__version__ = "0.0.4"

# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.


import os

from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305


class ChaCha20Poly1305Reusable:
    """A reuseable version of ChaCha20Poly1305.

    This is modified version of ChaCha20Poly1305 that does not recreate
    the underlying ctx each time. It is not thread-safe and should not
    only be called in the thread it was created.

    The primary use case for this code is HAP streams.
    """

    _MAX_SIZE = 2**32
    _KEY_LEN = 32
    _NONCE_LEN = 12

    def __init__(self, key: bytes | bytearray) -> None:
        if not isinstance(key, (bytes, bytearray)):
            raise TypeError("key must be bytes or bytearay")

        if len(key) != self._KEY_LEN:
            raise ValueError("ChaCha20Poly1305Reusable key must be 32 bytes.")

        self._key = bytes(key)
        self._cipher_name = b"chacha20-poly1305"
        self.name = "chacha20-poly1305"
        # Android's OpenSSL/BoringSSL integration can assert when probed through
        # cryptography's internal backend hooks. Use the public AEAD API instead.
        self._cipher = ChaCha20Poly1305(self._key)

    @classmethod
    def generate_key(cls) -> bytes:
        return os.urandom(cls._KEY_LEN)

    def encrypt(
        self,
        nonce: bytes | bytearray,
        data: bytes,
        associated_data: bytes | None,
    ) -> bytes:
        if associated_data is None:
            associated_data = b""

        if len(data) > self._MAX_SIZE or len(associated_data) > self._MAX_SIZE:
            # This is OverflowError to match what cffi would raise
            raise OverflowError("Data or associated data too long. Max 2**32 bytes")

        self._check_params(nonce, data, associated_data)
        return self._cipher.encrypt(bytes(nonce), data, associated_data)

    def decrypt(
        self,
        nonce: bytes | bytearray,
        data: bytes,
        associated_data: bytes | None,
    ) -> bytes:
        if associated_data is None:
            associated_data = b""

        self._check_params(nonce, data, associated_data)
        return self._cipher.decrypt(bytes(nonce), data, associated_data)

    def _check_params(
        self,
        nonce: bytes | bytearray,
        data: bytes,
        associated_data: bytes,
    ) -> None:
        if not isinstance(nonce, (bytes, bytearray)):
            raise TypeError("Nonce must be bytes or bytearray")
        if not isinstance(data, bytes):
            raise TypeError("data must be bytes")
        if not isinstance(associated_data, bytes):
            raise TypeError("associated_data must be bytes")
        if len(nonce) != self._NONCE_LEN:
            raise ValueError("Nonce must be 12 bytes")
