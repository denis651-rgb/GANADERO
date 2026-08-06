# Gestión de usuarios

Un usuario con `USUARIO_CREAR` invita por correo desde `/usuarios`. La empresa se obtiene del JWT/contexto; roles y propiedades se validan contra esa empresa. El formulario no solicita UUID. El invitado define su contraseña en `/auth/aceptar-invitacion`.
