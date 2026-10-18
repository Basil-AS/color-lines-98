// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { customPreset } from '../src/appearance';
import { LookBuilder } from '../src/components/LookBuilder';

const renderBuilder = (custom = customPreset(false)) => {
  const onCustom = vi.fn();
  render(<LookBuilder lang="en" theme="custom" custom={custom} overrides={undefined} onCustom={onCustom} onUseCustom={() => {}} onApplyShare={() => {}} />);
  return onCustom;
};

describe('look builder reset', () => {
  afterEach(cleanup);

  it('is off while the colours are the defaults', () => {
    renderBuilder();
    expect((screen.getByRole('button', { name: 'Reset colours' }) as HTMLButtonElement).disabled).toBe(true);
  });

  it('brings the colours back to the defaults and keeps the light or dark surface', async () => {
    const custom = { ...customPreset(true), accent: '#123456' };
    const onCustom = renderBuilder(custom);
    await userEvent.click(screen.getByRole('button', { name: 'Reset colours' }));
    expect(onCustom).toHaveBeenCalledWith(customPreset(true));
  });
});
