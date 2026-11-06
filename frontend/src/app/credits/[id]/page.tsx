import { Suspense } from "react";
import { CreditsApp } from "@/components/credits/CreditsApp";

export default function CreditAccountPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <Suspense fallback={<p>Loading credit account…</p>}>
      <CreditAccountContent params={params} />
    </Suspense>
  );
}

async function CreditAccountContent({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <CreditsApp creditTakerId={id} />;
}
