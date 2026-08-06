insert into storage.buckets (id,name,public,file_size_limit,allowed_mime_types)
values ('ganadero-private','ganadero-private',false,5242880,array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public=false,file_size_limit=excluded.file_size_limit,
    allowed_mime_types=excluded.allowed_mime_types;

-- El backend usa service_role y omite RLS. No se concede acceso directo al cliente.
drop policy if exists "deny direct access to ganadero private" on storage.objects;
create policy "deny direct access to ganadero private" on storage.objects
for all to authenticated using (false) with check (false);
