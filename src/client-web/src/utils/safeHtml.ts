import DOMPurify from 'dompurify';

const ALLOWED_TAGS = ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'ol', 'li'];
const ALLOWED_ATTR = ['href'];

export function looksLikeHtml(value: string): boolean {
  if (!value) return false;
  return /<[a-z][\s\S]*>/i.test(value.trim());
}

export function sanitizeDescriptionHtml(value: string): string {
  if (!value) return '';
  return DOMPurify.sanitize(value, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false,
  });
}
