# Supabase Storage

Use el script en `database/supabase/storage`. Las rutas se construyen como `empresas/{empresaId}/usuarios/{usuarioId}/avatar/{uuid}.ext`. Se admiten JPEG, PNG y WebP hasta 5 MiB. El cliente recibe URLs firmadas, nunca URLs públicas permanentes.
