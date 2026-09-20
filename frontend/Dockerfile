# Image d'exécution du backoffice : les fichiers construits, servis par nginx,
# qui relaie aussi /api vers le backend. Une seule origine pour le navigateur,
# donc aucun CORS et le cookie de renouvellement suit naturellement.
FROM nginx:1.29-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/backoffice/browser /usr/share/nginx/html

EXPOSE 80
