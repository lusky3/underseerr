/**
 * Complimentary access window.
 *
 * `FREE_ACCESS_UNTIL` is a plain date (YYYY-MM-DD). Everyone gets hosted push
 * notifications through the end of that day (UTC) without a trial or license.
 * Unset, empty or malformed turns it off, so removing the variable and
 * redeploying is the kill switch.
 */
const DATE_ONLY = /^(\d{4})-(\d{2})-(\d{2})$/;

/** End of the given day in epoch milliseconds (UTC), or null if the value is not a real date. */
export function parseFreeAccessUntil(value: string | undefined): number | null {
    const match = DATE_ONLY.exec((value ?? '').trim());
    if (!match) return null;

    const [year, month, day] = [Number(match[1]), Number(match[2]), Number(match[3])];
    const endOfDay = Date.UTC(year, month - 1, day, 23, 59, 59, 999);
    const check = new Date(endOfDay);

    // Reject rollovers such as 2027-02-31.
    if (check.getUTCFullYear() !== year || check.getUTCMonth() !== month - 1 || check.getUTCDate() !== day) {
        return null;
    }
    return endOfDay;
}

/** The end of the complimentary window if it is still open at `now`, otherwise null. */
export function complimentaryUntil(value: string | undefined, now: number): number | null {
    const end = parseFreeAccessUntil(value);
    return end !== null && now <= end ? end : null;
}
