import { createHmac } from 'node:crypto';

/**
 * Code à six chiffres, calculé comme le ferait le téléphone du gérant (RFC 6238).
 *
 * <p>Écrit ici plutôt qu'emprunté à une bibliothèque : la recette doit pouvoir
 * franchir le second facteur sans qu'on lui ouvre une porte dérobée. Le jour où
 * elle ne le peut plus, c'est le parcours réel qui est cassé.
 */
export const codeTotp = (secretBase32: string, instant = Date.now()): string => {
  const cle = decoderBase32(secretBase32);
  const compteur = Buffer.alloc(8);
  compteur.writeBigUInt64BE(BigInt(Math.floor(instant / 1000 / 30)));

  const empreinte = createHmac('sha1', cle).update(compteur).digest();
  const decalage = empreinte[empreinte.length - 1] & 0x0f;
  const tronque = empreinte.readUInt32BE(decalage) & 0x7fffffff;

  return String(tronque % 1_000_000).padStart(6, '0');
};

/** Secondes restantes avant que le code ne change. */
export const secondesRestantes = (instant = Date.now()): number =>
  30 - (Math.floor(instant / 1000) % 30);

const ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';

const decoderBase32 = (valeur: string): Buffer => {
  let bits = 0;
  let accumulateur = 0;
  const octets: number[] = [];

  for (const caractere of valeur.replace(/=+$/, '').toUpperCase()) {
    const index = ALPHABET.indexOf(caractere);
    if (index < 0) {
      throw new Error(`Secret TOTP invalide : caractère « ${caractere} »`);
    }
    accumulateur = (accumulateur << 5) | index;
    bits += 5;
    if (bits >= 8) {
      bits -= 8;
      octets.push((accumulateur >> bits) & 0xff);
    }
  }
  return Buffer.from(octets);
};
