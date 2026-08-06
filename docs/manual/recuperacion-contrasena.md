# Recuperación de contraseña

La solicitud siempre muestra una respuesta neutra. Supabase redirige a `/auth/restablecer-contrasena`, donde se valida la sesión temporal, se exige una contraseña de al menos diez caracteres y se cierra la sesión al finalizar.
