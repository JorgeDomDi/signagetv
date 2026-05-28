interface Props {
  value: string; // HH:mm
  onChange: (v: string) => void;
  id?: string;
}

export function TimePicker({ value, onChange, id }: Props) {
  // Normalizar a HH:mm (input type="time" requiere ese formato)
  const normalized = value ? value.slice(0, 5) : '';
  return (
    <input
      id={id}
      type="time"
      className="input"
      value={normalized}
      onChange={(e) => onChange(e.target.value)}
      step={60}
      required
    />
  );
}
