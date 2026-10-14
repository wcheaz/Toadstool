export interface ClientRecord {
  name: string;
  username: string;
  account: string;
  portfolio: string;
  cash: string;
  activity: string;
  activityTime: string;
  status: 'ACTIVE' | 'REVIEW';
  email: string;
  accountType: string;
  memberSince: string;
  security: string;
}

export const CLIENTS: ClientRecord[] = [
  { name: 'Alex Morgan', username: '@alex_morgan', account: 'TT-008631', portfolio: '$142,384.50', cash: '$12,450.00', activity: 'NEMS buy filled', activityTime: 'Today, 10:14 UTC', status: 'ACTIVE', email: 'alex.morgan@gmail.com', accountType: 'Individual · Cash', memberSince: '12 Jan 2024', security: '2FA enabled' },
  { name: 'Alexandra Reed', username: '@alex_reed', account: 'TT-007204', portfolio: '$86,210.00', cash: '$8,100.00', activity: 'Deposit completed', activityTime: 'Today, 09:52 UTC', status: 'ACTIVE', email: 'alexandra.reed@gmail.com', accountType: 'Individual · Cash', memberSince: '08 Nov 2023', security: '2FA enabled' },
  { name: 'Alex Chen', username: '@alex_chen', account: 'TT-009118', portfolio: '$23,604.20', cash: '$3,420.00', activity: 'KYC document uploaded', activityTime: 'Today, 09:08 UTC', status: 'REVIEW', email: 'alex.chen@gmail.com', accountType: 'Individual · Cash', memberSince: '03 Mar 2024', security: '2FA enabled' },
  { name: 'Alexis Rivera', username: '@alexis_r', account: 'TT-008542', portfolio: '$58,930.75', cash: '$6,240.00', activity: 'Successful sign-in', activityTime: 'Tuesday, 16:40 UTC', status: 'ACTIVE', email: 'alexis.rivera@gmail.com', accountType: 'Individual · Cash', memberSince: '21 Dec 2023', security: '2FA enabled' },
  { name: 'Priya Nair', username: '@priya_nair', account: 'TT-006417', portfolio: '$194,720.25', cash: '$21,680.00', activity: 'Portfolio review completed', activityTime: 'Today, 08:41 UTC', status: 'ACTIVE', email: 'priya.nair@gmail.com', accountType: 'Individual · Margin', memberSince: '19 Sep 2023', security: '2FA enabled' },
  { name: 'Jordan Williams', username: '@jordan_w', account: 'TT-010283', portfolio: '$41,875.60', cash: '$5,230.00', activity: 'New order submitted', activityTime: 'Yesterday, 18:22 UTC', status: 'ACTIVE', email: 'jordan.williams@gmail.com', accountType: 'Individual · Cash', memberSince: '14 Apr 2024', security: '2FA enabled' },
  { name: 'Mateo Silva', username: '@mateo_silva', account: 'TT-005906', portfolio: '$312,440.10', cash: '$38,900.00', activity: 'Withdrawal requested', activityTime: 'Yesterday, 15:07 UTC', status: 'REVIEW', email: 'mateo.silva@gmail.com', accountType: 'Individual · Margin', memberSince: '27 Jun 2023', security: '2FA enabled' },
  { name: 'Hannah Okafor', username: '@hannah_okafor', account: 'TT-011004', portfolio: '$17,265.40', cash: '$2,890.00', activity: 'Profile updated', activityTime: '21 Oct 2024, 12:36 UTC', status: 'ACTIVE', email: 'hannah.okafor@gmail.com', accountType: 'Individual · Cash', memberSince: '02 Aug 2024', security: '2FA enabled' },
  { name: 'Ethan Brooks', username: '@ethan_brooks', account: 'TT-004772', portfolio: '$73,508.90', cash: '$9,415.00', activity: 'Security update', activityTime: '20 Oct 2024, 10:18 UTC', status: 'ACTIVE', email: 'ethan.brooks@gmail.com', accountType: 'Individual · Cash', memberSince: '06 May 2023', security: '2FA enabled' }
];