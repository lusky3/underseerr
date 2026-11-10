import test from 'node:test';
import assert from 'node:assert/strict';
import { complimentaryUntil, parseFreeAccessUntil } from './access.ts';

const END_2027 = Date.UTC(2027, 11, 31, 23, 59, 59, 999);

test('a date means the end of that day in UTC', () => {
    assert.equal(parseFreeAccessUntil('2027-12-31'), END_2027);
    assert.equal(parseFreeAccessUntil(' 2027-12-31 '), END_2027);
});

test('unset, empty or malformed values turn the window off', () => {
    for (const value of [undefined, '', '   ', 'tomorrow', '2027-12', '12/31/2027', '2027-13-01', '2027-02-31']) {
        assert.equal(parseFreeAccessUntil(value), null, String(value));
    }
});

test('the window is open through the last millisecond of the final day', () => {
    assert.equal(complimentaryUntil('2027-12-31', Date.UTC(2026, 10, 10)), END_2027);
    assert.equal(complimentaryUntil('2027-12-31', END_2027), END_2027);
});

test('the window closes after the final day', () => {
    assert.equal(complimentaryUntil('2027-12-31', END_2027 + 1), null);
});

test('no variable means no complimentary access', () => {
    assert.equal(complimentaryUntil(undefined, Date.UTC(2026, 10, 10)), null);
});
