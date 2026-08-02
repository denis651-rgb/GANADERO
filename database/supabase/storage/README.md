# Storage privado de Supabase

Ejecute `create_private_bucket.sql` manualmente en el SQL Editor del proyecto correcto. El script crea o asegura el bucket privado `ganadero-private`, limita tamaño/MIME y deniega acceso directo del cliente.

El backend opera con `SUPABASE_SERVICE_ROLE_KEY`, que omite RLS. Por ello valida empresa, usuario, entidad y ruta antes de cada operación. Nunca exponga esa clave como variable `VITE_`.

Valide que el bucket figure como privado, intente una lectura anónima (debe fallar) y confirme que una URL firmada expire.
